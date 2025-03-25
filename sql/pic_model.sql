/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50722
 Source Host           : localhost:3306
 Source Schema         : pic_model

 Target Server Type    : MySQL
 Target Server Version : 50722
 File Encoding         : 65001

 Date: 25/03/2025 18:14:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_manage
-- ----------------------------
DROP TABLE IF EXISTS `file_manage`;
CREATE TABLE `file_manage`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET gbk COLLATE gbk_chinese_ci NULL DEFAULT NULL,
  `file_type` varchar(255) CHARACTER SET gbk COLLATE gbk_chinese_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `file_address` varchar(255) CHARACTER SET gbk COLLATE gbk_chinese_ci NULL DEFAULT NULL,
  `file_size` decimal(10, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = gbk COLLATE = gbk_chinese_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of file_manage
-- ----------------------------
INSERT INTO `file_manage` VALUES (5, 'test.png', 'png', '2025-03-25 17:47:35', 'D:/HFCOMPANY/picPath/1742896054530_test.png', 229.39);
INSERT INTO `file_manage` VALUES (6, 'test02.jpg', 'jpg', '2025-03-25 17:48:40', 'D:/HFCOMPANY/picPath/1742896120109_test02.jpg', 119.35);
INSERT INTO `file_manage` VALUES (7, 'test02 - 副本.jpg', 'jpg', '2025-03-25 17:59:43', 'D:/HFCOMPANY/picPath/1742896782624_test02 - 副本.jpg', 119.35);

SET FOREIGN_KEY_CHECKS = 1;
